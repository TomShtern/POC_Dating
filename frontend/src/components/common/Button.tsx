/**
 * Button Component
 *
 * PURPOSE: Reusable button component with variants and loading state
 *
 * PROPS:
 * - children: React.ReactNode
 * - onClick: () => void
 * - variant: 'primary' | 'secondary' | 'danger' | 'outline'
 * - size: 'sm' | 'md' | 'lg'
 * - isLoading: boolean
 * - disabled: boolean
 * - className: string (additional tailwind classes)
 * - type: 'button' | 'submit' | 'reset'
 *
 * STYLING (Tailwind):
 * - primary: bg-primary text-white
 * - secondary: bg-secondary text-white
 * - danger: bg-red-500 text-white
 * - outline: border-2 border-primary text-primary
 *
 * STATES:
 * - Default: Normal appearance
 * - Hover: Slightly darker background
 * - Disabled: Grayed out, no pointer
 * - Loading: Show spinner, disable button
 *
 * USAGE:
 * <Button onClick={handleClick} variant="primary" size="md">
 *   Click Me
 * </Button>
 */
export function Button() {
  // TODO: Implement Button component with all variants
  return <button>Button</button>
}
