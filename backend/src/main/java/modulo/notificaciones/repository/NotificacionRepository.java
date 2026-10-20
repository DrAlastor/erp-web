package modulo.notificaciones.repository;

import modulo.notificaciones.entity.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    List<Notificacion> findByUsuarioIdOrderByFechaCreacionDesc(Long usuarioId);

    List<Notificacion> findAllByOrderByFechaCreacionDesc();

    @Modifying
    @Query("UPDATE Notificacion n SET n.leido = true WHERE n.id = :id")
    void marcarLeida(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Notificacion n SET n.leido = true WHERE n.usuarioId = :usuarioId")
    void marcarTodasLeidasPorUsuario(@Param("usuarioId") Long usuarioId);

    @Modifying
    @Query("UPDATE Notificacion n SET n.leido = true")
    void marcarTodasLeidas();
}
