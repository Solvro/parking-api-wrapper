package pl.wrapper.parking.infrastructure.inMemory;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.*;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.springframework.core.serializer.support.SerializationFailedException;
import org.springframework.scheduling.annotation.Scheduled;

public abstract class InMemoryRepositoryImpl<K extends Serializable, V extends Serializable>
        implements InMemoryRepository<K, V> {

    protected final transient File file;
    protected Map<K, V> dataMap;
    protected V defaultValue;

    public InMemoryRepositoryImpl(String filePath, Map<K, V> map, V defaultValue) {
        this.file = new File(filePath);
        this.defaultValue = defaultValue;

        this.dataMap = map;
    }

    public Collection<V> values() {
        return dataMap.values();
    }

    @Override
    public void add(K key, V value) {
        dataMap.put(key, value);
    }

    @Override
    public Set<K> fetchAllKeys() {
        return Collections.unmodifiableSet(dataMap.keySet());
    }

    @Override
    public Set<Map.Entry<K, V>> fetchAllEntries() {
        return dataMap.entrySet();
    }

    @Override
    public V get(K key) {
        return dataMap.getOrDefault(key, defaultValue);
    }

    @PostConstruct
    @SuppressWarnings("unchecked")
    protected void init() {
        if (!file.exists()) return;

        try (FileInputStream fileOut = new FileInputStream(file);
                ObjectInputStream in = new ObjectInputStream(fileOut)) {

            this.dataMap = (Map<K, V>) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new SerializationFailedException(createExceptionForIOE("Deserialization", e));
        }
    }

    @PreDestroy
    private void selfSerialize() {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists())
            if (!parent.mkdirs())
                throw new SerializationFailedException(
                        "Failed to create directory for path: " + file.getAbsolutePath());

        try (FileOutputStream fileOut = new FileOutputStream(file)) {
            ObjectOutputStream out = new ObjectOutputStream(fileOut);

            out.writeObject(dataMap);
        } catch (IOException e) {
            throw new SerializationFailedException(createExceptionForIOE("Serialization", e));
        }
    }

    @Scheduled(fixedRateString = "#{60 * 1000 * ${serialization.timeStamp.inMinutes}}")
    protected void periodicSerialize() {
        selfSerialize();
    }

    private static <E extends Exception> String createExceptionForIOE(String methodType, E e) {
        return methodType + " failed for: " + InMemoryRepositoryImpl.class.getSimpleName() + ". Message: "
                + e.getMessage();
    }
}
