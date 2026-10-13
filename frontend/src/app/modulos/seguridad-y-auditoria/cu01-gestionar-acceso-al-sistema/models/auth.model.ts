export interface LoginRequest {
  usernameOrEmail: string;
  password: string;
}

export interface ClienteRegisterRequest {
  username: string;
  email: string;
  password: string;
  razonSocial: string;
  nitCi: string;
  telefono: string;
  direccion: string;
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
