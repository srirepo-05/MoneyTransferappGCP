/**
 * Utility validators for form inputs.
 */

export const validators = {
  required: (value) =>
    value !== undefined && value !== null && String(value).trim() !== ''
      ? null
      : 'This field is required.',

  minLength: (min) => (value) =>
    String(value).trim().length >= min
      ? null
      : `Must be at least ${min} characters.`,

  maxLength: (max) => (value) =>
    String(value).trim().length <= max
      ? null
      : `Must be at most ${max} characters.`,

  numeric: (value) =>
    /^\d+(\.\d+)?$/.test(String(value).trim()) ? null : 'Must be a valid number.',

  positiveAmount: (value) => {
    const num = parseFloat(value);
    if (isNaN(num)) return 'Must be a valid number.';
    if (num <= 0) return 'Amount must be greater than zero.';
    if (!/^\d+(\.\d{1,2})?$/.test(String(value).trim()))
      return 'Amount must have at most 2 decimal places.';
    return null;
  },

  maxAmount: (max) => (value) => {
    const num = parseFloat(value);
    return num <= max ? null : `Amount cannot exceed $${max.toLocaleString()}.`;
  },

  accountNumber: (value) =>
    /^\d{8,16}$/.test(String(value).trim())
      ? null
      : 'Account number must be 8–16 digits.',

  /** Runs an array of validator functions and returns the first error found. */
  compose:
    (...fns) =>
    (value) => {
      for (const fn of fns) {
        const error = fn(value);
        if (error) return error;
      }
      return null;
    },
};

/** Validates a whole form object against a schema of { fieldName: validatorFn }. */
export function validateForm(values, schema) {
  const errors = {};
  for (const [field, validate] of Object.entries(schema)) {
    const error = validate(values[field]);
    if (error) errors[field] = error;
  }
  return errors;
}
