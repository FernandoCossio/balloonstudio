# PrimeNG Design Tokens — Referencia compacta (Aura preset)

> Todas las variables siguen el patrón `var(--p-<token>)`.  
> En el preset JS, la jerarquía de objetos se convierte a kebab-case: `colorScheme.light.primary.color` → `--p-primary-color`.

---

## 1. Primitivos — Paleta de colores

Todos los colores de Tailwind están disponibles con el prefijo `--p-`. Formato: `--p-{color}-{shade}` donde shade ∈ {50,100,200,300,400,500,600,700,800,900,950}.

| Familia       | Valores hex representativos (500)         |
|---------------|-------------------------------------------|
| `slate`       | #64748b — surface en light mode           |
| `zinc`        | #71717a — surface en dark mode            |
| `gray`        | #6b7280                                   |
| `neutral`     | #737373                                   |
| `stone`       | #78716c                                   |
| `red`         | #ef4444                                   |
| `orange`      | #f97316                                   |
| `amber`       | #f59e0b                                   |
| `yellow`      | #eab308                                   |
| `lime`        | #84cc16                                   |
| `green`       | #22c55e                                   |
| `emerald`     | #10b981 — primary por defecto en Aura     |
| `teal`        | #14b8a6                                   |
| `cyan`        | #06b6d4                                   |
| `sky`         | #0ea5e9 — usado en severity="info"        |
| `blue`        | #3b82f6                                   |
| `indigo`      | #6366f1                                   |
| `violet`      | #8b5cf6                                   |
| `purple`      | #a855f7 — usado en severity="help"        |
| `fuchsia`     | #d946ef                                   |
| `pink`        | #ec4899                                   |
| `rose`        | #f43f5e                                   |

**Ejemplo:** `var(--p-blue-200)`, `var(--p-slate-950)`

---

## 2. Primitivos — Border radius

| Token                      | Valor  |
|----------------------------|--------|
| `--p-border-radius-none`   | 0      |
| `--p-border-radius-xs`     | 2px    |
| `--p-border-radius-sm`     | 4px    |
| `--p-border-radius-md`     | 6px    |
| `--p-border-radius-lg`     | 8px    |
| `--p-border-radius-xl`     | 12px   |

---

## 3. Semánticos — Primary (tu preset: marrón #8D4E2D)

| Token                         | Light                    | Dark                |
|-------------------------------|--------------------------|---------------------|
| `--p-primary-{50…950}`        | Tu paleta personalizada  | (misma paleta)      |
| `--p-primary-color`           | `--p-primary-500`        | `--p-primary-400`   |
| `--p-primary-contrast-color`  | #ffffff                  | surface-900         |
| `--p-primary-hover-color`     | `--p-primary-600`        | `--p-primary-300`   |
| `--p-primary-active-color`    | `--p-primary-700`        | `--p-primary-200`   |

---

## 4. Semánticos — Colores de severidad (tu preset)

| Prefijo       | 500 (base)  | Rango disponible  |
|---------------|-------------|-------------------|
| `--p-success` | #2E7D32     | 50…950            |
| `--p-warning` | #F57C00     | 50…950            |
| `--p-danger`  | #D32F2F     | 50…950            |
| `--p-info`    | #0288D1     | 50…950            |

**Ejemplo:** `var(--p-success-100)`, `var(--p-danger-500)`

---

## 5. Semánticos — Surface (escala neutral adaptativa)

`--p-surface-{0|50|100…950}` mapea a `slate` en light y `zinc` en dark automáticamente.

| Token              | Uso típico                        |
|--------------------|-----------------------------------|
| `--p-surface-0`    | #ffffff (siempre blanco)          |
| `--p-surface-50`   | Fondos muy sutiles                |
| `--p-surface-100`  | Hover de items de lista/nav       |
| `--p-surface-200`  | Bordes, inputs disabled           |
| `--p-surface-300`  | Borde de inputs                   |
| `--p-surface-400`  | Íconos, placeholders              |
| `--p-surface-500`  | Texto muted, placeholders         |
| `--p-surface-600`  | Texto secundario                  |
| `--p-surface-700`  | Texto principal (light)           |
| `--p-surface-800`  | Texto hover                       |
| `--p-surface-900`  | Fondos oscuros                    |
| `--p-surface-950`  | Contraste máximo                  |

---

## 6. Semánticos — Texto y contenido

| Token                          | Valor                        |
|--------------------------------|------------------------------|
| `--p-text-color`               | `--p-surface-700`            |
| `--p-text-hover-color`         | `--p-surface-800`            |
| `--p-text-muted-color`         | `--p-surface-500`            |
| `--p-text-hover-muted-color`   | `--p-surface-600`            |
| `--p-content-background`       | `--p-surface-0`              |
| `--p-content-hover-background` | `--p-surface-100`            |
| `--p-content-border-color`     | `--p-surface-200`            |
| `--p-content-color`            | `--p-text-color`             |
| `--p-content-hover-color`      | `--p-text-hover-color`       |
| `--p-content-border-radius`    | `--p-border-radius-md`       |

---

## 7. Semánticos — Highlight / selección

| Token                           | Light                  |
|---------------------------------|------------------------|
| `--p-highlight-background`      | `--p-primary-50`       |
| `--p-highlight-focus-background`| `--p-primary-100`      |
| `--p-highlight-color`           | `--p-primary-700`      |
| `--p-highlight-focus-color`     | `--p-primary-800`      |

---

## 8. Semánticos — Generales

| Token                       | Valor             |
|-----------------------------|-------------------|
| `--p-transition-duration`   | 0.2s              |
| `--p-disabled-opacity`      | 0.6               |
| `--p-icon-size`             | 1rem              |
| `--p-anchor-gutter`         | 2px               |
| `--p-mask-background`       | rgba(0,0,0,0.4)   |
| `--p-mask-color`            | `--p-surface-200` |
| `--p-mask-transition-duration` | 0.3s           |

---

## 9. Focus ring

| Token                    | Valor                  |
|--------------------------|------------------------|
| `--p-focus-ring-width`   | 1px                    |
| `--p-focus-ring-style`   | solid                  |
| `--p-focus-ring-color`   | `--p-primary-color`    |
| `--p-focus-ring-offset`  | 2px                    |
| `--p-focus-ring-shadow`  | none                   |

---

## 10. Form fields

| Token                                  | Valor                        |
|----------------------------------------|------------------------------|
| `--p-form-field-padding-x`             | 0.75rem                      |
| `--p-form-field-padding-y`             | 0.5rem                       |
| `--p-form-field-border-radius`         | `--p-border-radius-md`       |
| `--p-form-field-transition-duration`   | `--p-transition-duration`    |
| `--p-form-field-background`            | `--p-surface-0`              |
| `--p-form-field-disabled-background`   | `--p-surface-200`            |
| `--p-form-field-filled-background`     | `--p-surface-50`             |
| `--p-form-field-border-color`          | `--p-surface-300`            |
| `--p-form-field-hover-border-color`    | `--p-surface-400`            |
| `--p-form-field-focus-border-color`    | `--p-primary-color`          |
| `--p-form-field-invalid-border-color`  | `--p-red-400`                |
| `--p-form-field-color`                 | `--p-surface-700`            |
| `--p-form-field-disabled-color`        | `--p-surface-500`            |
| `--p-form-field-placeholder-color`     | `--p-surface-500`            |
| `--p-form-field-invalid-placeholder-color` | `--p-red-600`            |
| `--p-form-field-float-label-color`     | `--p-surface-500`            |
| `--p-form-field-float-label-focus-color` | `--p-primary-600`          |
| `--p-form-field-icon-color`            | `--p-surface-400`            |
| `--p-form-field-shadow`                | 0 1px 2px rgba(18,18,23,.05) |
| `--p-form-field-sm-font-size`          | 0.875rem                     |
| `--p-form-field-sm-padding-x`          | 0.625rem                     |
| `--p-form-field-sm-padding-y`          | 0.375rem                     |
| `--p-form-field-lg-font-size`          | 1.125rem                     |
| `--p-form-field-lg-padding-x`          | 0.875rem                     |
| `--p-form-field-lg-padding-y`          | 0.625rem                     |

---

## 11. Overlays

| Token                              | Valor                          |
|------------------------------------|--------------------------------|
| `--p-overlay-select-background`    | `--p-surface-0`                |
| `--p-overlay-select-border-color`  | `--p-surface-200`              |
| `--p-overlay-select-color`         | `--p-text-color`               |
| `--p-overlay-select-border-radius` | `--p-border-radius-md`         |
| `--p-overlay-select-shadow`        | 0 4px 6px -1px rgba(0,0,0,.1) |
| `--p-overlay-popover-background`   | `--p-surface-0`                |
| `--p-overlay-popover-border-color` | `--p-surface-200`              |
| `--p-overlay-popover-color`        | `--p-text-color`               |
| `--p-overlay-popover-border-radius`| `--p-border-radius-md`         |
| `--p-overlay-popover-padding`      | 0.75rem                        |
| `--p-overlay-popover-shadow`       | 0 4px 6px -1px rgba(0,0,0,.1) |
| `--p-overlay-modal-background`     | `--p-surface-0`                |
| `--p-overlay-modal-border-color`   | `--p-surface-200`              |
| `--p-overlay-modal-color`          | `--p-text-color`               |
| `--p-overlay-modal-border-radius`  | `--p-border-radius-xl`         |
| `--p-overlay-modal-padding`        | 1.25rem                        |
| `--p-overlay-modal-shadow`         | 0 20px 25px -5px rgba(0,0,0,.1)|
| `--p-overlay-navigation-shadow`    | 0 4px 6px -1px rgba(0,0,0,.1) |

---

## 12. Navegación

| Token                                    | Valor                      |
|------------------------------------------|----------------------------|
| `--p-navigation-item-padding`            | 0.5rem 0.75rem             |
| `--p-navigation-item-border-radius`      | `--p-border-radius-sm`     |
| `--p-navigation-item-gap`                | 0.5rem                     |
| `--p-navigation-item-color`              | `--p-text-color`           |
| `--p-navigation-item-focus-color`        | `--p-text-hover-color`     |
| `--p-navigation-item-active-color`       | `--p-text-hover-color`     |
| `--p-navigation-item-focus-background`   | `--p-surface-100`          |
| `--p-navigation-item-active-background`  | `--p-surface-100`          |
| `--p-navigation-item-icon-color`         | `--p-surface-400`          |
| `--p-navigation-item-icon-focus-color`   | `--p-surface-500`          |
| `--p-navigation-item-icon-active-color`  | `--p-surface-500`          |
| `--p-navigation-list-padding`            | 0.25rem                    |
| `--p-navigation-list-gap`                | 2px                        |
| `--p-navigation-submenu-label-padding`   | 0.5rem 0.75rem             |
| `--p-navigation-submenu-label-font-weight` | 600                      |
| `--p-navigation-submenu-label-color`     | `--p-text-muted-color`     |
| `--p-navigation-submenu-icon-size`       | 0.875rem                   |
| `--p-navigation-submenu-icon-color`      | `--p-surface-400`          |

---

## 13. Listas (dropdowns, selects, autocomplete)

| Token                                  | Valor                            |
|----------------------------------------|----------------------------------|
| `--p-list-padding`                     | 0.25rem                          |
| `--p-list-gap`                         | 2px                              |
| `--p-list-header-padding`              | 0.5rem 1rem 0.25rem 1rem         |
| `--p-list-option-padding`              | 0.5rem 0.75rem                   |
| `--p-list-option-border-radius`        | `--p-border-radius-sm`           |
| `--p-list-option-color`                | `--p-text-color`                 |
| `--p-list-option-focus-color`          | `--p-text-hover-color`           |
| `--p-list-option-focus-background`     | `--p-surface-100`                |
| `--p-list-option-selected-background`  | `--p-highlight-background`       |
| `--p-list-option-selected-focus-background` | `--p-highlight-focus-background`|
| `--p-list-option-selected-color`       | `--p-highlight-color`            |
| `--p-list-option-selected-focus-color` | `--p-highlight-focus-color`      |
| `--p-list-option-icon-color`           | `--p-surface-400`                |
| `--p-list-option-icon-focus-color`     | `--p-surface-500`                |
| `--p-list-option-group-padding`        | 0.5rem 0.75rem                   |
| `--p-list-option-group-font-weight`    | 600                              |
| `--p-list-option-group-color`          | `--p-text-muted-color`           |

---

## 14. Componentes — Button (tokens clave)

Patrón: `--p-button-{severity}-{state}-{property}`  
Severities: `primary` | `secondary` | `success` | `info` | `warn` | `help` | `danger` | `contrast`  
States: *(vacío)* | `hover` | `active`  
Properties: `background` | `border-color` | `color` | `focus-ring-color`

| Token destacado                        | Valor                         |
|----------------------------------------|-------------------------------|
| `--p-button-primary-background`        | `--p-primary-color`           |
| `--p-button-primary-color`             | `--p-primary-contrast-color`  |
| `--p-button-primary-hover-background`  | `--p-primary-hover-color`     |
| `--p-button-secondary-background`      | `--p-surface-100`             |
| `--p-button-secondary-color`           | `--p-surface-600`             |
| `--p-button-border-radius`             | `--p-form-field-border-radius`|
| `--p-button-gap`                       | 0.5rem                        |
| `--p-button-padding-x`                 | `--p-form-field-padding-x`    |
| `--p-button-padding-y`                 | `--p-form-field-padding-y`    |
| `--p-button-icon-only-width`           | 2.5rem                        |
| `--p-button-label-font-weight`         | 500                           |

Variantes text/outlined: `--p-button-text-{severity}-color`, `--p-button-outlined-{severity}-color`

---

## 15. Componentes — Tooltip

| Token                      | Valor                           |
|----------------------------|---------------------------------|
| `--p-tooltip-background`   | `--p-surface-700`               |
| `--p-tooltip-color`        | `--p-surface-0`                 |
| `--p-tooltip-padding`      | 0.5rem 0.75rem                  |
| `--p-tooltip-border-radius`| `--p-overlay-popover-border-radius` |
| `--p-tooltip-max-width`    | 12.5rem                         |
| `--p-tooltip-shadow`       | `--p-overlay-popover-shadow`    |
| `--p-tooltip-gutter`       | 0.25rem                         |

---

## 16. Componentes — Menu

| Token                          | Valor                              |
|--------------------------------|------------------------------------|
| `--p-menu-background`          | `--p-content-background`           |
| `--p-menu-border-color`        | `--p-content-border-color`         |
| `--p-menu-color`               | `--p-content-color`                |
| `--p-menu-border-radius`       | `--p-content-border-radius`        |
| `--p-menu-shadow`              | `--p-overlay-navigation-shadow`    |
| `--p-menu-item-*`              | Hereda de `--p-navigation-item-*`  |
| `--p-menu-list-*`              | Hereda de `--p-navigation-list-*`  |
| `--p-menu-separator-border-color` | `--p-content-border-color`      |

---

## Notas de uso

- **Dark mode**: PrimeNG aplica `.app-dark` (según tu config). Los tokens `--p-surface-*`, `--p-primary-*` y colores semánticos se redefinen automáticamente en esa clase. No hace falta redefinir nada en tu CSS.
- **Sobreescribir un token puntualmente**: basta con redefinir la variable en el selector deseado: `.mi-componente { --p-button-border-radius: 2rem; }`
- **En el preset JS**: usa `{token.path}` como valor para referenciar otros tokens, ej: `'{primary.color}'`, `'{surface.200}'`.
