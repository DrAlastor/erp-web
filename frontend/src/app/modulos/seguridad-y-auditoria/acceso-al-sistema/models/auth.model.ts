export interface LoginRequest {
  usernameOrEmail: string;
  password: string;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface UsuarioPerfil {
  id: number;
  username: string;
  email: string;
  fullname: string;
}

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  usuario: UsuarioPerfil;
}
