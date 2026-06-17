export interface UserSummary {
  id: number;
  name: string;
  email: string;
  username: string;
  role: "USER" | "ADMIN" | "ROOT";
}
