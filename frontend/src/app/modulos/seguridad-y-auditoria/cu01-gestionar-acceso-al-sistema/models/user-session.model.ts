import { UsuarioPerfil } from './auth.model';

export interface UserSession {
  accessToken: string;
  refreshToken: string;
  usuario: UsuarioPerfil;
}
