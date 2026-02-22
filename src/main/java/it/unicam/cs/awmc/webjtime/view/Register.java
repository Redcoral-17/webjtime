package it.unicam.cs.awmc.webjtime.view;

import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import it.unicam.cs.awmc.webjtime.service.UserService;

import static it.unicam.cs.awmc.webjtime.view.MainLayout.showError;
import static it.unicam.cs.awmc.webjtime.view.MainLayout.showSuccess;

@Route("register")
@PageTitle("Registrazione")
@AnonymousAllowed
public class Register extends VerticalLayout {
    private static final String FIELD_WIDTH = "20em";
    private final UserService uService;
    private final TextField uField = new TextField("Username");
    private final PasswordField pwField = new PasswordField("Password");
    private final PasswordField confirmPwField = new PasswordField("Conferma password");

    public Register(UserService uService) {
        this.uService = uService;
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        setSizeFull();
        for (var field : new AbstractField[]{uField, pwField, confirmPwField}) {
            ((HasSize) field).setWidth(FIELD_WIDTH);
        }
        uField.setRequired(true);
        pwField.setRequired(true);
        confirmPwField.setRequired(true);
        Button registerBtn = new Button("Registrati", e -> handleRegister());
        registerBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        registerBtn.setWidth(FIELD_WIDTH);
        Button goToLogin = new Button("Hai già un account? Accedi", e -> UI.getCurrent().navigate(Login.class));
        goToLogin.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        add(uField, pwField, confirmPwField, registerBtn, goToLogin);
    }

    private void handleRegister() {
        if (uField.getValue().isBlank() || pwField.getValue().isBlank() || confirmPwField.getValue().isBlank()) {
            showError("Tutti i campi sono obbligatori");
            return;
        }
        if (!pwField.getValue().equals(confirmPwField.getValue())) {
            showError("Le password non coincidono");
            return;
        }
        try {
            uService.register(uField.getValue(), pwField.getValue());
            showSuccess("Registrazione completata! Ora puoi effettuare il login.");
            UI.getCurrent().navigate(Login.class);
        } catch (IllegalArgumentException ex) { showError(ex.getMessage()); }
    }
}



