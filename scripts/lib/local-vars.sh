#!/bin/sh

require_sops_identity() {
  if [ -n "${SOPS_AGE_KEY:-}" ] || [ -n "${SOPS_AGE_KEY_FILE:-}" ] || [ -n "${SOPS_AGE_KEY_CMD:-}" ] || \
     [ -n "${SOPS_AGE_SSH_PRIVATE_KEY_FILE:-}" ] || [ -n "${SOPS_AGE_SSH_PRIVATE_KEY_CMD:-}" ]; then
    return
  fi

  if [ -f "$HOME/.ssh/id_ed25519" ] || [ -f "$HOME/.ssh/id_rsa" ] || \
     [ -f "$HOME/.config/sops/age/keys.txt" ] || [ -f "$HOME/Library/Application Support/sops/age/keys.txt" ]; then
    return
  fi

  echo "No SOPS identity found. Set SOPS_AGE_SSH_PRIVATE_KEY_FILE or SOPS_AGE_KEY_FILE, or install a standard SOPS key." >&2
  exit 1
}
