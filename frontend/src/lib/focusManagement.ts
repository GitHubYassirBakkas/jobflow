export const focusableSelector = [
  "a[href]",
  "button:not([disabled])",
  "input:not([disabled]):not([type='hidden'])",
  "select:not([disabled])",
  "textarea:not([disabled])",
  "[tabindex]:not([tabindex='-1'])"
].join(",");

export function getNextFocusIndex(
  currentIndex: number,
  itemCount: number,
  isShiftKey: boolean
) {
  if (itemCount <= 0) {
    return -1;
  }

  if (currentIndex < 0) {
    return isShiftKey ? itemCount - 1 : 0;
  }

  if (isShiftKey) {
    return currentIndex === 0 ? itemCount - 1 : currentIndex - 1;
  }

  return currentIndex === itemCount - 1 ? 0 : currentIndex + 1;
}

function isElementFocusable(element: HTMLElement) {
  const ariaHidden = element.getAttribute("aria-hidden") === "true";
  const isDisabled = "disabled" in element && Boolean(element.disabled);

  return !ariaHidden && !isDisabled && element.tabIndex >= 0;
}

export function getFocusableElements(container: HTMLElement) {
  return Array.from(container.querySelectorAll<HTMLElement>(focusableSelector))
    .filter(isElementFocusable);
}

export function focusFirstElement(
  container: HTMLElement | null,
  preferredElement?: HTMLElement | null
) {
  if (!container) {
    return;
  }

  if (preferredElement && isElementFocusable(preferredElement)) {
    preferredElement.focus();
    return;
  }

  const [firstElement] = getFocusableElements(container);

  if (firstElement) {
    firstElement.focus();
    return;
  }

  container.focus();
}

export function trapTabKey(event: KeyboardEvent, container: HTMLElement | null) {
  if (event.key !== "Tab" || !container) {
    return false;
  }

  const focusableElements = getFocusableElements(container);
  const nextFocusIndex = getNextFocusIndex(
    focusableElements.indexOf(document.activeElement as HTMLElement),
    focusableElements.length,
    event.shiftKey
  );

  event.preventDefault();

  if (nextFocusIndex === -1) {
    container.focus();
    return true;
  }

  focusableElements[nextFocusIndex].focus();
  return true;
}
