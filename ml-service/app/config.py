"""Configuracion del servicio, leida de variables de entorno o de ml-service/.env."""
from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Configuracion(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_prefix="ML_", extra="ignore")

    clave_interna: str = ""
    master_database_url: str = ""
    tenant_db_usuario: str = ""
    tenant_db_clave: str = ""
    tenant_db_host_por_defecto: str = ""
    tenant_db_sslmode: str = "require"
    zona_horaria: str = "America/La_Paz"


@lru_cache
def configuracion() -> Configuracion:
    return Configuracion()
