export interface UserSummary {
  id: number;
  name: string;
  email: string;
  username: string;
  role: "USER" | "ADMIN" | "ROOT";
}

interface SubMenu {
  subMenuCode: string;
}

export interface MenuFeature {
  featureCode: string;
  displayOrder: number;
  hidden: boolean;
  subMenus: SubMenu[];
}

export interface MenuModule {
  moduleCode: string;
  displayOrder: number;
  hidden: boolean;
  features: MenuFeature[];
}

export interface UserMenu {
  modules: MenuModule[];
}
