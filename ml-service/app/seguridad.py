"""Autenticacion interna: solo el backend Spring conoce la clave compartida."""
import hmac

from fastapi import Header, HTTPException, status

from app.config import configuracion


def verificar_clave(x_clave_interna: str | None = Header(default=None)) -> None:
    esperada = configuracion().clave_interna
    # Falla cerrado: sin clave configurada nadie pasa.
    if not esperada or not x_clave_interna or not hmac.compare_digest(
        x_clave_interna.encode("utf-8"), esperada.encode("utf-8")
    ):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Clave interna invalida")
