export interface LoginRequest {
  codigoEmpresa?: string;
  usernameOrEmail: string;
  password: string;
}

export interface ClienteRegisterRequest {
  codigoEmpresa: string;
  username: string;
  email: string;
  password: string;
  razonSocial: string;
  nitCi: string;
  telefono: string;
  direccion: string;
}

export interface RefreshTokenRequest {
  codigoEmpresa: string;
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
  codigoEmpresa?: string;
  usuario: UsuarioPerfil;
}
