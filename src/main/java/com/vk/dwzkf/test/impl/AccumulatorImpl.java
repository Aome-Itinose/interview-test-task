package com.vk.dwzkf.test.impl;

import com.vk.dwzkf.test.Accumulator;
import com.vk.dwzkf.test.State;
import com.vk.dwzkf.test.StateObject;

import java.util.*;

/**
 * @author Roman Shageev
 * @since 12.08.2024
 */
public class AccumulatorImpl implements Accumulator {
    private final Map<Long, ArrayList<StateObject>> unorderedProcessesStates = new HashMap<>();
    private final Map<Long, State> processesLastStates = new HashMap<>();

    @Override
    public void accept(StateObject stateObject) {
        ArrayList<StateObject> currentUnorderedStates = getCurrentUnorderedStates(stateObject.getProcessId());
        currentUnorderedStates.add(stateObject);
    }

    /**
     * Возвращает невыгруженные процессы
     * @param processId , или пустой список
     */
    private ArrayList<StateObject> getCurrentUnorderedStates(Long processId) {
        if (!unorderedProcessesStates.containsKey(processId)) {
            unorderedProcessesStates.put(processId, new ArrayList<>());
        }

        return unorderedProcessesStates.get(processId);
    }

    @Override
    public void acceptAll(List<StateObject> stateObjects) {
        stateObjects.forEach(this::accept);
    }

    @Override
    public List<StateObject> drain(Long processId) {
        return orderStates(processId);
    }

    private ArrayList<StateObject> orderStates(Long processId) {
        ArrayList<StateObject> currentProcessStates = unorderedProcessesStates.get(processId);
        ArrayList<StateObject> result = new ArrayList<>();

        if (!processesLastStates.containsKey(processId)) {
            orderStartStatesIfExist(processId, currentProcessStates, result);
        }
        if (processesLastStates.containsKey(processId) &&
                !processesLastStates.get(processId).equals(State.FINAL1) &&
                !processesLastStates.get(processId).equals(State.FINAL2)) {

            orderMidStatesIfExist(processId, currentProcessStates, result);
            orderFinalStatesIfExist(processId, currentProcessStates, result);
        }
        unorderedProcessesStates.remove(processId);
        return result;
    }

    private void orderStartStatesIfExist(Long processId, ArrayList<StateObject> currentProcessStates, ArrayList<StateObject> result) {
        List<StateObject> starts;
        if (currentProcessStates.stream().anyMatch(e -> e.getState().equals(State.START1))) {
            starts = currentProcessStates.stream().filter(e -> e.getState().equals(State.START1)).toList();
            result.add(starts.get(0));

            processesLastStates.put(processId, State.START1);

        } else if (currentProcessStates.stream().anyMatch(e -> e.getState().equals(State.START2))) {
            starts = currentProcessStates.stream().filter(e -> e.getState().equals(State.START2)).toList();
            result.add(starts.get(0));

            processesLastStates.put(processId, State.START2);

        }
    }
    private void orderMidStatesIfExist(Long processId, ArrayList<StateObject> currentProcessStates, ArrayList<StateObject> result) {
        if (currentProcessStates.stream().anyMatch(e -> e.getState().equals(State.MID1) || e.getState().equals(State.MID2))) {
            List<StateObject> mid1List = currentProcessStates.stream().filter(e -> e.getState().equals(State.MID1)).toList();
            List<StateObject> mid2List = new ArrayList<>(currentProcessStates.stream().filter(e -> e.getState().equals(State.MID2)).toList());

            State lastState = processesLastStates.get(processId);
            if (lastState.equals(State.MID1) && !mid2List.isEmpty()) {
                result.add(mid2List.get(0));
                mid2List.remove(0);
                lastState = State.MID2;
            }

            int pairCounts = Math.min(mid2List.size(), mid1List.size());
            int i;
            for (i = 0; i < pairCounts; i++) {
                result.add(mid1List.get(i));
                result.add(mid2List.get(i));
                lastState = State.MID2;
            }
            if (i < mid1List.size() && !lastState.equals(State.MID1)) {
                result.add(mid1List.get(i));

                lastState = State.MID1;
            }
            processesLastStates.put(processId, lastState);
        }
    }
    private void orderFinalStatesIfExist(Long processId, ArrayList<StateObject> currentProcessStates, ArrayList<StateObject> result) {
        List<StateObject> finals;
        if(currentProcessStates.stream().anyMatch(e -> e.getState().equals(State.FINAL1))){
            finals = currentProcessStates.stream().filter(e -> e.getState().equals(State.FINAL1)).toList();
            result.add(finals.get(0));
            processesLastStates.put(processId, State.FINAL1);
        }else if(currentProcessStates.stream().anyMatch(e -> e.getState().equals(State.FINAL2))){
            finals = currentProcessStates.stream().filter(e -> e.getState().equals(State.FINAL2)).toList();
            result.add(finals.get(0));
            processesLastStates.put(processId, State.FINAL2);
        }
    }
}
