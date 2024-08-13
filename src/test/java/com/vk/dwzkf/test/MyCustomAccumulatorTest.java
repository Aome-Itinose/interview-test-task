package com.vk.dwzkf.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static com.vk.dwzkf.test.State.*;

public class MyCustomAccumulatorTest {
    private static final AccumulatorFactory factory = new AccumulatorFactory();
    private static final AtomicLong counter = new AtomicLong();
    private final Accumulator accumulator = factory.getInstance();

    @Test
    public void case1() {
        Long processId = counter.getAndIncrement();
        accumulator.acceptAll(buildList(processId, MID1, MID1, MID2, START1));
        List<StateObject> actual = accumulator.drain(processId);
        checkSequenceNumbers(actual, 4, 1, 3, 2);
        checkStates(actual, START1, MID1, MID2, MID1);
    }
    @Test
    public void case2() {
        Long processId = counter.getAndIncrement();
        accumulator.acceptAll(buildList(processId, MID1, MID1, MID2));
        List<StateObject> actual = accumulator.drain(processId);
        checkSequenceNumbers(actual);
    }
    @Test
    public void case3() {
        Long processId = counter.getAndIncrement();
        accumulator.acceptAll(buildList(processId, START2, MID1, START1));
        List<StateObject> actual = accumulator.drain(processId);
        checkSequenceNumbers(actual, 3, 2);
        checkStates(actual, START1, MID1);

        accumulator.acceptAll(buildList(processId, MID1, FINAL1));
        actual = accumulator.drain(processId);
        checkSequenceNumbers(actual, 5);
        checkStates(actual, FINAL1);
    }

    private void checkStates(List<StateObject> stateObjects, State... expected) {
        State[] actual = stateObjects.stream()
                .map(StateObject::getState)
                .toArray(State[]::new);
        Assertions.assertArrayEquals(actual, expected);
    }

    private void checkSequenceNumbers(List<StateObject> stateObjects, Integer... expected) {
        Integer[] actual = stateObjects.stream()
                .map(StateObject::getSeqNo)
                .toArray(Integer[]::new);
        Assertions.assertArrayEquals(expected, actual);
    }

    private List<StateObject> buildList(Long processId, State... states) {
        return Arrays.stream(states)
                .map(state -> new StateObject(processId, state))
                .collect(Collectors.toList());
    }
}
