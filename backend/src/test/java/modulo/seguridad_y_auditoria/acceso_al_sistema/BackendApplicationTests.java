package modulo.seguridad_y_auditoria.acceso_al_sistema;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = comun.BackendApplication.class)
@ActiveProfiles("local")
class BackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
