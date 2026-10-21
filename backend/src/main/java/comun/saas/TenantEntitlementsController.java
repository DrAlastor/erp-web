package comun.saas;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/saas")
public class TenantEntitlementsController {

    private final TenantEntitlementsService service;

    public TenantEntitlementsController(TenantEntitlementsService service) {
        this.service = service;
    }

    @GetMapping("/mi-suscripcion")
    public TenantEntitlementsResponse current() {
        return service.current();
    }
}
