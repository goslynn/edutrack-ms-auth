package cl.duocuc.edutrack.ms.auth;

import cl.duocuc.edutrack.ms.infrastructure.discovery.ServiceIds;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

@ApplicationPath("/"+ServiceIds.AUTH)
public class AuthApplication extends Application {
}
