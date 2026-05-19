export const ROLE_ADMINISTRADOR = 'role_administrador' as const;
export const ROLE_CLIENTE = 'role_cliente' as const;
export const ROLE_EMPLEADO = 'role_empleado' as const;

export const ROLES = {
    ADMINISTRADOR: ROLE_ADMINISTRADOR,
    CLIENTE: ROLE_CLIENTE,
    EMPLEADO: ROLE_EMPLEADO
} as const;

export type Role = typeof ROLES[keyof typeof ROLES];
