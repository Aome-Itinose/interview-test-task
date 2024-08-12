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
    private final Map<Long, ArrayDeque<StateObject>> processes = new HashMap<>();
    private final Map<Long, State> processesLastStates = new HashMap<>();

    @Override
    public void accept(StateObject stateObject) {
        ArrayDeque<StateObject> currentProcessStates = getCurrentProcessStates(stateObject.getProcessId());

        if (isValidStateObject(stateObject, currentProcessStates)) {
            currentProcessStates.addLast(stateObject);
        }
    }

    /**
     * Если есть еще не выгруженные уведомления у процесса
     * @param processId , возвращает
     */
    private ArrayDeque<StateObject> getCurrentProcessStates(Long processId) {
        if(!processes.containsKey(processId)){
            processes.put(processId, new ArrayDeque<>());
        }

        return processes.get(processId);
    }

    @Override
    public void acceptAll(List<StateObject> stateObjects) {
        stateObjects.forEach(this::accept);
    }

    @Override
    public List<StateObject> drain(Long processId) {
        ArrayDeque<StateObject> currentProcessStates = processes.get(processId);

        updateLastProcessStates(processId, currentProcessStates);
        processes.remove(processId);
        return currentProcessStates.stream().toList();
    }

    /**
     * Обновляет последнее состояние текущего процесса, если состояние менялось.
     */
    private void updateLastProcessStates(Long processId, ArrayDeque<StateObject> currentProcessStates) {
        if(!currentProcessStates.isEmpty()) {
            processesLastStates.put(processId, currentProcessStates.getLast().getState());
        }
    }

    /**
     * Проверяет на валидность уведомление
     */
    private boolean isValidStateObject(StateObject stateObject, ArrayDeque<StateObject> currentProcessStates) {
        if(currentProcessStates.isEmpty()) {
            return isFirstStateInAcceptValid(stateObject);
        }else{
            return isNextStateInAcceptValid(stateObject, currentProcessStates);
        }
    }
    /**
     * Проверка на валидность, если после выгрузки еще не было валидных состояний
     */
    private boolean isFirstStateInAcceptValid(StateObject stateObject) {
        if(!processesLastStates.containsKey(stateObject.getProcessId())) {
            return stateObject.getState().equals(State.START1) || stateObject.getState().equals(State.START2);
        }

        State lastState = processesLastStates.get(stateObject.getProcessId());
        switch (stateObject.getState()) {
            case START1, START2 -> {
                return false;
            }
            case MID1 -> {
                return lastState.equals(State.START1) ||
                        lastState.equals(State.START2) ||
                        lastState.equals(State.MID2);
            }
            case MID2 -> {
                return lastState.equals(State.MID1);
            }
            case FINAL1, FINAL2 -> {
                return !(lastState.equals(State.FINAL1) || lastState.equals(State.FINAL2));
            }
        }
        return false;
    }
    /**
     * Проверка на валидность, если после выгрузки уже были валидные состояния
     */
    private boolean isNextStateInAcceptValid(StateObject stateObject, ArrayDeque<StateObject> currentProcessStates) {
        State lastState = currentProcessStates.getLast().getState();

        switch (stateObject.getState()) {
            case START1 -> {
                if(lastState.equals(State.START2)) {
                    currentProcessStates.removeLast();
                    return true;
                }
                return false;
            }
            case START2 -> {
                return false;
            }
            case MID1 -> {
                return lastState.equals(State.START1) ||
                        lastState.equals(State.START2) ||
                        lastState.equals(State.MID2);
            }
            case MID2 -> {
                return lastState.equals(State.MID1);
            }
            case FINAL1 -> {
                if(lastState.equals(State.FINAL2)) {
                    currentProcessStates.removeLast();
                    return true;
                }
                return !lastState.equals(State.FINAL1);
            }
            case FINAL2 -> {
                return !lastState.equals(State.FINAL2);
            }
        }
        return false;
    }
}
