package pl.wrapper.parking.infrastructure.inMemory;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ParkingRequestRepositoryTest {

    private InMemoryRepositoryTest inMemoryRepository;
    private Integer id;
    private DummyObject object;
    private static final String path = "data/statistics/tests";

    static class InMemoryRepositoryTest extends ParkingDataRepository {
        public InMemoryRepositoryTest() {
            super(path);
        }

        public void testSerialize() {
            periodicSerialize();
        }

        public void testDeserialize() {
            init();
        }

        public void deleteData() {
            dataMap.clear();
        }
    }

    @BeforeEach
    void setUp() {
        inMemoryRepository = new InMemoryRepositoryTest();
        object = new DummyObject();
        id = 10;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @AfterEach
    void tearDown() {
        File file = new File(path);
        file.delete();
    }

    @Test
    void shouldReturnObject() {
        inMemoryRepository.add(id, object);

        int first = inMemoryRepository.fetchAllKeys().stream().findFirst().orElseThrow();

        assertEquals(first, id);
        assertEquals(inMemoryRepository.get(first), object);
    }

    @Test
    void shouldSerializationRunCorrectly() {
        inMemoryRepository.add(id, object);

        inMemoryRepository.testSerialize();
        inMemoryRepository.deleteData();

        assertTrue(inMemoryRepository.fetchAllKeys().isEmpty());

        inMemoryRepository.testDeserialize();
        assertEquals(inMemoryRepository.get(id).s, object.s);
    }
}
