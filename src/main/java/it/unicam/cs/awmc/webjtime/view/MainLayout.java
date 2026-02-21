package it.unicam.cs.awmc.webjtime.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Objects;

/**
 * Layout principale dell'applicazione Vaadin.
 * Fornisce la navigazione laterale verso Calendar, Projects e Reports.
 *
 * @author Filippo Corallini (125587), filippo.corallini@studenti.unicam.it
 */
@Layout
@PermitAll
public class MainLayout extends AppLayout {

    public MainLayout() {
        createHeader();
        createDrawer();
    }

    private void createHeader() {
        H1 title = new H1("JTime4Web");
        title.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.MEDIUM);
        HorizontalLayout header = new HorizontalLayout(new DrawerToggle(), title);
        header.add(logoutBtn);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.setWidthFull();
        header.addClassNames(LumoUtility.Padding.Vertical.NONE, LumoUtility.Padding.Horizontal.MEDIUM);
        addToNavbar(header);
    }

    private final Button logoutBtn = new Button("Logout", e -> {
        ServletRequestAttributes attr = (ServletRequestAttributes)
                RequestContextHolder.currentRequestAttributes();
        HttpServletRequest req = attr.getRequest();
        HttpServletResponse res = attr.getResponse();
        new SecurityContextLogoutHandler().logout(req, Objects.requireNonNull(res), null);
        UI.getCurrent().getPage().setLocation("/login");
    });

    private void createDrawer() {
        SideNav nav = new SideNav();
        nav.addItem(new SideNavItem("Calendar", Calendar.class));
        nav.addItem(new SideNavItem("Project", ProjectList.class));
        nav.addItem(new SideNavItem("Report", ReportList.class));
        addToDrawer(nav);
    }

    public static void showError(String message) {
        Notification n = Notification.show(message, 3000, Notification.Position.MIDDLE);
        n.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    public static void showSuccess(String message) {
        Notification n = Notification.show(message, 3000, Notification.Position.MIDDLE);
        n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }
}