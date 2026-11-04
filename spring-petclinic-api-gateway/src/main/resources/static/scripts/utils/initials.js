/**
 * Builds a two-letter monogram used by the circular avatars.
 */
export function initials(firstName, lastName) {
    const first = (firstName || '').trim().charAt(0);
    const last = (lastName || '').trim().charAt(0);
    return (first + last).toUpperCase() || '?';
}
