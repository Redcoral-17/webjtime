package it.unicam.cs.awmc.webjtime.view;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import java.util.function.Consumer;
/**
 * Utility per la creazione uniforme di dialog modali con FormLayout e footer standard.
 *
 * @author Filippo Corallini (125587), filippo.corallini@studenti.unicam.it
 */
public final class DialogBuilder {
    private DialogBuilder() {}

    public static void build(String title, Consumer<Dialog> onSave, Component... fields) {
        build(title, "Save", onSave, fields);
    }

    public static void build(String title, String confirmLabel, Consumer<Dialog> onSave, Component... fields) {
        Dialog dialog = new Dialog(title);
        dialog.add(new FormLayout(fields));
        Button cancel  = new Button("Cancel", e -> dialog.close());
        Button confirm = new Button(confirmLabel, e -> onSave.accept(dialog));
        confirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.getFooter().add(cancel, confirm);
        dialog.open();
    }
}