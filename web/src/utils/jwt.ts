export interface JwtPayload {
  sub: string;
  name: string;
  role: string;
  exp: number;
  mustChangePassword?: boolean;
}

export function decodeJwtPayload(token: string): JwtPayload | null {
  try {
    const payload = token.split(".")[1];
    return JSON.parse(Buffer.from(payload, "base64url").toString());
  } catch {
    return null;
  }
}
