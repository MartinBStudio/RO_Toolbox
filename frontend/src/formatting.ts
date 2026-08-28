export function capitalizeFirstLetter(value: string | null | undefined) {
  if (!value) return value;
  return value.charAt(0).toUpperCase() + value.slice(1);
}
