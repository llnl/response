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
package gov.llnl.gnem.response;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.jcs.JCS;
import org.apache.commons.jcs.access.exception.CacheException;
import org.apache.commons.jcs.access.CacheAccess;

/**
 *
 * @author dodge1
 */
public class CachedResponseHolder {

    private CacheAccess<ToResponseLookupKey, InverseTransferFunction> cache = null;
    private CacheAccess<FromResponseLookupKey, TransferData> forwardCache = null;
    private Map<Long, ResponseMetaData> metadataCache = null;
    private AtomicLong inverseTransferHitCount;
    private AtomicLong inverseTransferMissCount;
    private AtomicLong forwardTransferHitCount;
    private AtomicLong forwardTransferMissCount;
    private AtomicLong metadataMissCount;
    private AtomicLong metadataHitCount;

    private CachedResponseHolder() {
        try {
            cache = JCS.getInstance("default");
            forwardCache = JCS.getInstance("default");
            metadataCache = new ConcurrentHashMap<>();
            inverseTransferHitCount = new AtomicLong(0);
            inverseTransferMissCount = new AtomicLong(0);
            forwardTransferHitCount = new AtomicLong(0);
            forwardTransferMissCount = new AtomicLong(0);
            metadataMissCount = new AtomicLong(0);
            metadataHitCount = new AtomicLong(0);
        } catch (CacheException e) {
            Logger.getLogger(CachedResponseHolder.class.getName()).log(Level.FINE, e.getMessage());
        }
    }

    public void resetHitCounts() {
        inverseTransferHitCount.getAndSet(0L);
        inverseTransferMissCount.getAndSet(0L);
        forwardTransferHitCount.getAndSet(0L);
        forwardTransferMissCount.getAndSet(0L);
        metadataMissCount.getAndSet(0L);
        metadataHitCount.getAndSet(0L);
    }

    public long getInverseTransferHitCount() {
        return inverseTransferHitCount.get();
    }

    public long getInverseTransferMissCount() {
        return inverseTransferMissCount.get();
    }

    public long getForwardTransferHitCount() {
        return forwardTransferHitCount.get();
    }

    public long getForwardTransferMissCount() {
        return forwardTransferMissCount.get();
    }

    public long getMetadataHitCount() {
        return metadataHitCount.get();
    }

    public long getMetadataMissCount() {
        return metadataMissCount.get();
    }

    public static CachedResponseHolder getInstance() {
        return CachedResponseHolderHolder.INSTANCE;
    }

    public InverseTransferFunction retrieveInverseTransferFunction(ToResponseLookupKey key, double time) {
        if (!key.contains(time)) {
            inverseTransferMissCount.incrementAndGet();
            return null;
        }
        InverseTransferFunction result = cache.get(key);
        if (result == null) {
            inverseTransferMissCount.incrementAndGet();
        } else {
            inverseTransferHitCount.incrementAndGet();
        }
        return result;
    }

    public void cacheInverseTransferFunction(ToResponseLookupKey key, InverseTransferFunction result) {
        cache.put(key, result);
    }

    public TransferData retrieveForwardTransferFunction(FromResponseLookupKey key, Double time) {
        if (!key.contains(time)) {
            forwardTransferMissCount.incrementAndGet();
            return null;
        }
        TransferData result = forwardCache.get(key);
        if (result == null) {
            forwardTransferMissCount.incrementAndGet();
        } else {
            forwardTransferHitCount.incrementAndGet();
        }
        return result;
    }

    public void cacheForwardTransferFunction(FromResponseLookupKey key, TransferData result) {
        forwardCache.put(key, result);
    }

    public ResponseMetaData retrieveMetadata(Long waveformId) {
        ResponseMetaData result = metadataCache.get(waveformId);
        if (result == null) {
            metadataMissCount.incrementAndGet();
        } else {
            metadataHitCount.incrementAndGet();
        }
        return result;
    }

    public void cacheMetadata(Long waveformId, ResponseMetaData data) {
        metadataCache.put(waveformId, data);
    }

    public String getStateString() {
        return String.format("Forward: %d hits, %d misses; Inverse: %d hits, %d misses; Metadata: %d hits, %d misses",
                this.getForwardTransferHitCount(),
                this.getForwardTransferMissCount(),
                this.getInverseTransferHitCount(),
                this.getInverseTransferMissCount(),
                this.getMetadataHitCount(),
                this.getMetadataMissCount());
    }

    private static class CachedResponseHolderHolder {

        private static final CachedResponseHolder INSTANCE = new CachedResponseHolder();
    }
}
