import { UsuarioPerfil } from './auth.model';

export interface UserSession {
  codigoEmpresa: string;
  accessToken: string;
  refreshToken: string;
  usuario: UsuarioPerfil;
}
