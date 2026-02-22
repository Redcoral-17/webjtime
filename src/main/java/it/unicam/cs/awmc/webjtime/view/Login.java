package it.unicam.cs.awmc.webjtime.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("login")
@PageTitle("Login")
@AnonymousAllowed
public class Login extends VerticalLayout {

    public Login() {
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        setSizeFull();
        LoginForm login = new LoginForm();
        login.setAction("login");
        Button registerBtn = new Button("Non hai un account? Registrati",
                e ->  UI.getCurrent().navigate(Register.class));
        registerBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        add(login, registerBtn);
    }

}
