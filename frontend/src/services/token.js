let _accessToken = null

export const tokenStore = {
  get: () => _accessToken,
  set: (token) => { _accessToken = token },
  clear: () => { _accessToken = null },
}
