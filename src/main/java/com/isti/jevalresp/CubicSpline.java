/*-
 * #%L
 * Seismic Response Processing Module
 *  LLNL-CODE-856351
 *  This work was performed under the auspices of the U.S. Department of Energy
 *  by Lawrence Livermore National Laboratory under Contract DE-AC52-07NA27344.
 * %%
 * Copyright (C) 2023 Lawrence Livermore National Laboratory
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.isti.jevalresp;

public class CubicSpline {
    public static double TRIG_ARG_MIN = 0.001D;

    public static double TRIG_ARG_MAX = 50.0D;

    public double[] calcSpline(double[] t, double[] y, double tension, double k, double[] xValsArr) {
        if (t.length != y.length) {
            throw new IllegalArgumentException("Arrays 't[]' and 'y[]' not same length");
        }
        int used = t.length - 1;
        int numXVals = xValsArr.length;
        if (used < 1 || numXVals <= 0) {
            return new double[0];
        }
        if (used <= 1) {
            k = 0.0D;
        }
        if (!isMonotonic(t)) {
            throw new IllegalArgumentException("Abscissa values not monotonic");
        }
        double[] z = fit(t, y, k, tension, false);
        if (z == null) {
            return null;
        }
        double lastXVal = xValsArr[numXVals - 1];
        int lastVal = 0;
        if (lastXVal == t[0]) {
            lastVal = 1;
        } else if (lastXVal == t[used]) {
            lastVal = 2;
        }
        double[] retValsArr = new double[numXVals];
        int retArrIdx = 0;
        int outRangeCount = 0;
        for (int i = 0; i < numXVals; i++) {
            double x = xValsArr[i];
            if (i == numXVals - 1) {
                if (lastVal == 1) {
                    x = t[0];
                } else if (lastVal == 2) {
                    x = t[used];
                }
            }
            if ((x - t[0]) * (x - t[used]) <= 0.0D) {
                retValsArr[retArrIdx++] = interpolate(t, y, z, x, tension, false);
            } else {
                outRangeCount++;
            }
        }
        if (outRangeCount > 0) {
            throw new IllegalArgumentException(outRangeCount + " requested point" + ((outRangeCount != 1) ? "s" : "") + " could not be computed (out of data range)");
        }
        return retValsArr;
    }

    public double[] fit(double[] t, double[] y, double k, double tension, boolean periodic) {
        if (t.length != y.length) {
            throw new IllegalArgumentException("Arrays 't[]' and 'y[]' not same length in 'fit' method");
        }
        int n = t.length - 1;
        if (n == 1) {
            return new double[] { 0.0D, 0.0D };
        }
        double[] z = new double[n + 1];
        double[] h = new double[n];
        double[] b = new double[n];
        double[] u = new double[n];
        double[] v = new double[n];
        double[] alpha = new double[n];
        double[] beta = new double[n];
        double[] uu = null, vv = null, s = null;
        if (periodic) {
            s = new double[n];
            uu = new double[n];
            vv = new double[n];
        }
        int i;
        for (i = 0; i <= n - 1; i++) {
            h[i] = t[i + 1] - t[i];
            b[i] = 6.0D * (y[i + 1] - y[i]) / h[i];
        }
        if (tension < 0.0D) {
            for (i = 0; i <= n - 1; i++) {
                if (Math.sin(tension * h[i]) == 0.0D) {
                    throw new IllegalArgumentException("Specified negative tension value is singular in 'fit' method");
                }
            }
        }
        if (tension == 0.0D) {
            for (i = 0; i <= n - 1; i++) {
                alpha[i] = h[i];
                beta[i] = 2.0D * h[i];
            }
        } else if (tension > 0.0D) {
            for (i = 0; i <= n - 1; i++) {
                double x = tension * h[i];
                double xabs = (x < 0.0D) ? -x : x;
                if (xabs < TRIG_ARG_MIN) {
                    alpha[i] = h[i] * sinh_func(x);
                    beta[i] = 2.0D * h[i] * tanh_func(x);
                } else if (xabs > TRIG_ARG_MAX) {
                    int sign = (x < 0.0D) ? -1 : 1;
                    alpha[i] = 6.0D / tension * tension * (1.0D / h[i] - tension * 2.0D * sign * Math.exp(-xabs));
                    beta[i] = 6.0D / tension * tension * (tension - 1.0D / h[i]);
                } else {
                    alpha[i] = 6.0D / tension * tension * (1.0D / h[i] - tension / sinh(x));
                    beta[i] = 6.0D / tension * tension * (tension / tanh(x) - 1.0D / h[i]);
                }
            }
        } else {
            for (i = 0; i <= n - 1; i++) {
                double x = tension * h[i];
                double xabs = (x < 0.0D) ? -x : x;
                if (xabs < TRIG_ARG_MIN) {
                    alpha[i] = h[i] * sin_func(x);
                    beta[i] = 2.0D * h[i] * tan_func(x);
                } else {
                    alpha[i] = 6.0D / tension * tension * (1.0D / h[i] - tension / Math.sin(x));
                    beta[i] = 6.0D / tension * tension * (tension / Math.tan(x) - 1.0D / h[i]);
                }
            }
        }
        if (!periodic && n == 2) {
            u[1] = beta[0] + beta[1] + 2.0D * k * alpha[0];
        } else {
            u[1] = beta[0] + beta[1] + k * alpha[0];
        }
        v[1] = b[1] - b[0];
        if (u[1] == 0.0D) {
            throw new IllegalArgumentException("As posed, problem of computing spline is singular in 'fit' method");
        }
        if (periodic) {
            s[1] = alpha[0];
            uu[1] = 0.0D;
            vv[1] = 0.0D;
        }
        for (i = 2; i <= n - 1; i++) {
            u[i] = beta[i] + beta[i - 1] - alpha[i - 1] * alpha[i - 1] / u[i - 1] + ((i == n - 1) ? (k * alpha[n - 1]) : 0.0D);
            if (u[i] == 0.0D) {
                throw new IllegalArgumentException("As posed, problem of computing spline is singular in 'fit' method");
            }
            v[i] = b[i] - b[i - 1] - alpha[i - 1] * v[i - 1] / u[i - 1];
            if (periodic) {
                s[i] = -s[i - 1] * alpha[i - 1] / u[i - 1];
                uu[i] = uu[i - 1] - s[i - 1] * s[i - 1] / u[i - 1];
                vv[i] = vv[i - 1] - v[i - 1] * s[i - 1] / u[i - 1];
            }
        }
        if (!periodic) {
            z[n] = 0.0D;
            for (i = n - 1; i >= 1; i--) {
                z[i] = (v[i] - alpha[i] * z[i + 1]) / u[i];
            }
            z[0] = 0.0D;
            z[0] = k * z[1];
            z[n] = k * z[n - 1];
        } else {
            z[n - 1] = (v[n - 1] + vv[n - 1]) / (u[n - 1] + uu[n - 1] + 2.0D * s[n - 1]);
            for (i = n - 2; i >= 1; i--) {
                z[i] = (v[i] - alpha[i] * z[i + 1] - s[i] * z[n - 1]) / u[i];
            }
            z[0] = z[n - 1];
            z[n] = z[1];
        }
        return z;
    }

    public static double interpolate(double[] t, double[] y, double[] z, double x, double tension, boolean periodic) {
        double value;
        int n = (t.length <= y.length) ? (t.length - 1) : (y.length - 1);
        boolean is_ascending = (t[n - 1] < t[n]);
        int i = 0;
        if (periodic && (x - t[0]) * (x - t[n]) > 0.0D) {
            x -= (int) Math.floor((x - t[0]) / (t[n] - t[0])) * (t[n] - t[0]);
        }
        int k;
        for (k = n - i; k > 1;) {
            if (is_ascending ? (x >= t[i + (k >> 1)]) : (x <= t[i + (k >> 1)])) {
                i += k >> 1;
                k -= k >> 1;
                continue;
            }
            k >>= 1;
        }
        double h = t[i + 1] - t[i];
        double diff = x - t[i];
        double updiff = t[i + 1] - x;
        double reldiff = diff / h;
        double relupdiff = updiff / h;
        if (tension == 0.0D) {
            value = y[i] + diff * ((y[i + 1] - y[i]) / h - h * (z[i + 1] + z[i] * 2.0D) / 6.0D + diff * (0.5D * z[i] + diff * (z[i + 1] - z[i]) / 6.0D * h));
        } else if (tension > 0.0D) {
            if (Math.abs(tension * h) < TRIG_ARG_MIN) {
                value = y[i] * relupdiff + y[i + 1] * reldiff + z[i] * h * h / 6.0D * quotient_sinh_func(relupdiff, tension * h) + z[i + 1] * h * h / 6.0D * quotient_sinh_func(reldiff, tension * h);
            } else if (Math.abs(tension * h) > TRIG_ARG_MAX) {
                int sign = (h < 0.0D) ? -1 : 1;
                value = (z[i] * (Math.exp(tension * updiff - sign * tension * h) + Math.exp(-tension * updiff - sign * tension * h))
                        + z[i + 1] * (Math.exp(tension * diff - sign * tension * h) + Math.exp(-tension * diff - sign * tension * h))) * sign / tension * tension
                        + (y[i] - z[i] / tension * tension) * updiff / h
                        + (y[i + 1] - z[i + 1] / tension * tension) * diff / h;
            } else {
                value = (z[i] * sinh(tension * updiff) + z[i + 1] * sinh(tension * diff)) / tension * tension * sinh(tension * h)
                        + (y[i] - z[i] / tension * tension) * updiff / h
                        + (y[i + 1] - z[i + 1] / tension * tension) * diff / h;
            }
        } else if (Math.abs(tension * h) < TRIG_ARG_MIN) {
            value = y[i] * relupdiff + y[i + 1] * reldiff + z[i] * h * h / 6.0D * quotient_sin_func(relupdiff, tension * h) + z[i + 1] * h * h / 6.0D * quotient_sin_func(reldiff, tension * h);
        } else {
            value = (z[i] * Math.sin(tension * updiff) + z[i + 1] * Math.sin(tension * diff)) / tension * tension * Math.sin(tension * h)
                    + (y[i] - z[i] / tension * tension) * updiff / h
                    + (y[i + 1] - z[i + 1] / tension * tension) * diff / h;
        }
        return value;
    }

    public final boolean isMonotonic(double[] t) {
        boolean isAscending;
        int n = t.length - 1;
        if (n <= 0) {
            return false;
        }
        if (t[n - 1] < t[n]) {
            isAscending = true;
        } else if (t[n - 1] > t[n]) {
            isAscending = false;
        } else {
            return false;
        }
        while (n > 0) {
            n--;
            if (isAscending ? (t[n] >= t[n + 1]) : (t[n] <= t[n + 1])) {
                return false;
            }
        }
        return true;
    }

    public static double sinh(double value) {
        return (Math.exp(value) - Math.exp(-value)) / 2.0D;
    }

    public static double tanh(double value) {
        double expVal = Math.exp(value);
        double expNVal = Math.exp(-value);
        return (expVal - expNVal) / (expVal + expNVal);
    }

    public static double sinh_func(double x) {
        return 1.0D - 0.11666666666666667D * x * x + 0.012301587301587301D * x * x * x * x;
    }

    public static double tanh_func(double x) {
        return 1.0D - 0.06666666666666667D * x * x + 0.006349206349206349D * x * x * x * x;
    }

    public static double sin_func(double x) {
        return -1.0D - 0.11666666666666667D * x * x - 0.012301587301587301D * x * x * x * x;
    }

    public static double tan_func(double x) {
        return -1.0D - 0.06666666666666667D * x * x - 0.006349206349206349D * x * x * x * x;
    }

    public static double quotient_sinh_func(double x, double y) {
        return x * x * x
                - x
                + (x * x * x * x * x / 20.0D - x * x * x / 6.0D + 7.0D * x / 60.0D) * y * y
                + (x * x * x * x * x * x * x / 840.0D - x * x * x * x * x / 120.0D + 7.0D * x * x * x / 360.0D - 31.0D * x / 2520.0D) * y * y * y * y;
    }

    public static double quotient_sin_func(double x, double y) {
        return -(x * x * x - x)
                + (x * x * x * x * x / 20.0D - x * x * x / 6.0D + 7.0D * x / 60.0D) * y * y
                - (x * x * x * x * x * x * x / 840.0D - x * x * x * x * x / 120.0D + 7.0D * x * x * x / 360.0D - 31.0D * x / 2520.0D) * y * y * y * y;
    }
}
