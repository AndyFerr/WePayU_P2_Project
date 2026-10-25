package br.ufal.ic.p2.wepayu.persistence;

import br.ufal.ic.p2.wepayu.models.Employee;
import br.ufal.ic.p2.wepayu.schedule.PaymentSchedule;
import br.ufal.ic.p2.wepayu.schedule.Schedules;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Único dono do estado do sistema (empregados + contador de ids). O estado
 * inteiro pode ser copiado para bytes (snapshot) e restaurado, o que sustenta
 * tanto o undo/redo quanto a persistência em arquivo.
 */
public class EmployeeRepository {

    private static class State implements Serializable {
        final Map<String, Employee> employees = new LinkedHashMap<>();
        /** Agendas disponíveis no sistema; começa só com as 3 default (US 9/10). */
        final Map<String, PaymentSchedule> schedules = new LinkedHashMap<>();
        int nextId = 1;

        State() {
            schedules.put(Schedules.WEEKLY.getDescription(), Schedules.WEEKLY);
            schedules.put(Schedules.BIWEEKLY.getDescription(), Schedules.BIWEEKLY);
            schedules.put(Schedules.MONTHLY.getDescription(), Schedules.MONTHLY);
        }
    }

    private State state = new State();

    public String nextId() {
        return String.valueOf(state.nextId++);
    }

    public void add(Employee employee) {
        state.employees.put(employee.getId(), employee);
    }

    /** Substitui o empregado de mesmo id mantendo sua posição (usado na troca de tipo). */
    public void replace(Employee employee) {
        state.employees.put(employee.getId(), employee);
    }

    public void remove(String id) {
        state.employees.remove(id);
    }

    public Optional<Employee> findById(String id) {
        return Optional.ofNullable(state.employees.get(id));
    }

    /** Empregados com esse nome, na ordem de criação. */
    public List<Employee> findByName(String name) {
        List<Employee> result = new ArrayList<>();
        for (Employee e : state.employees.values()) {
            if (e.getName().equals(name)) {
                result.add(e);
            }
        }
        return result;
    }

    public Optional<Employee> findByUnionId(String unionId) {
        return state.employees.values().stream()
                .filter(e -> e.hasUnionMemberId(unionId))
                .findFirst();
    }

    public Collection<Employee> findAll() {
        return List.copyOf(state.employees.values());
    }

    public Optional<PaymentSchedule> findSchedule(String description) {
        return Optional.ofNullable(state.schedules.get(description));
    }

    /** Registra uma nova agenda, indexada pela sua própria descrição. */
    public void addSchedule(PaymentSchedule schedule) {
        state.schedules.put(schedule.getDescription(), schedule);
    }

    public void clear() {
        state = new State();
    }

    public byte[] snapshot() {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(state);
            out.flush();
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void restore(byte[] snapshot) {
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(snapshot))) {
            state = (State) in.readObject();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }
}