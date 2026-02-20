package it.unicam.cs.awmc.webjtime.model;

/**
 * Rappresenta lo stato (Status) di una task o di un progetto (T/P)
 * Se {@code status} è impostata su {@link Status#ACTIVE}, T/P è ancora in corso,
 * altrimenti è impostata su {@link Status#COMPLETED} e T/P è stata completata.
 *
 * @author Filippo Corallini (125587), filippo.corallini@studenti.unicam.it
 */
public enum Status {
    ACTIVE, COMPLETED
}