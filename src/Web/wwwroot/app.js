window.familyFit = (() => {
  window.addEventListener('hashchange', () => {
    if (window.location.pathname === '/login' && new URLSearchParams(window.location.hash.slice(1)).has('secret')) window.location.reload();
  });
  const headers = { 'X-FamilyFit-Request': '1', 'Content-Type': 'application/json' };
  let timer;
  let currentCode = '';
  const status = message => { const element = document.getElementById('login-status'); if (element) element.textContent = message; };
  const stop = () => { clearTimeout(timer); timer = undefined; };
  function takeLinkSecret() { const secret = new URLSearchParams(window.location.hash.slice(1)).get("secret"); if (secret) window.history.replaceState(null, "", window.location.pathname + window.location.search); return secret; }
  async function start(linkSecret) {
    stop();
    const qr = document.getElementById('login-qr');
    if (!qr) return;
    status('Préparation d’un nouveau code…');
    try {
      const response = await fetch('/api/v1/auth/browser/challenges', { method: 'POST', headers, credentials: 'same-origin', body: JSON.stringify({ linkSecret }) });
      if (!response.ok) throw new Error(response.status === 429 ? 'Trop de demandes. Réessaie dans une minute.' : 'Ce lien est expiré ou indisponible. Demande un nouveau lien à l’administrateur.');
      const data = await response.json();
      currentCode = data.code;
      qr.innerHTML = data.qrSvg;
      document.getElementById('login-code').textContent = `${currentCode.slice(0, 5)} ${currentCode.slice(5)}`;
      status('Confirme dans ton application avant ' + new Date(data.expiresAtUtc).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' }) + '.');
      const deadline = new Date(data.expiresAtUtc).getTime();
      let failures = 0;
      async function poll() {
        if (!document.querySelector('[data-login]')) return;
        if (Date.now() >= deadline) { status('Ce lien a expiré. Demande un nouveau lien à l’administrateur.'); return; }
        try {
          const answer = await fetch('/api/v1/auth/browser/consume', { method: 'POST', headers, credentials: 'same-origin' });
          if (!answer.ok) throw new Error('Connexion interrompue.');
          const result = await answer.json();
          failures = 0;
          if (result.state === 'approved') { status('Connexion confirmée. Bienvenue !'); window.location.replace('/'); return; }
          if (result.state === 'expired') { status('Ce lien n’est plus valide. Demande un nouveau lien à l’administrateur.'); return; }
        } catch {
          failures++;
          status('Le réseau est indisponible. Nouvelle tentative en cours…');
        }
        timer = setTimeout(poll, Math.min(10000, 2000 * (failures + 1)));
      }
      timer = setTimeout(poll, 2000);
    } catch (error) { status(error.message); }
  }
  async function copy() {
    if (!currentCode) return;
    try { await navigator.clipboard.writeText(currentCode); status('Code copié. Saisis-le dans ton application FamilyFit.'); }
    catch { status('Saisis les dix caractères du code dans ton application.'); }
  }
  async function logout() {
    const response = await fetch('/api/v1/auth/logout', { method: 'POST', headers, credentials: 'same-origin' });
    if (response.ok || response.status === 401) window.location.replace('/login');
  }
  function revealSelectedDay() {
    const scroll = document.querySelector('.calendar-scroll');
    const day = scroll?.querySelector('.selected-day');
    if (!day) return;
    const left = day.offsetLeft - scroll.offsetLeft - (scroll.clientWidth - day.clientWidth) / 2;
    scroll.scrollTo({ left: Math.max(0, left), behavior: 'instant' });
  }
  async function copyText(text) { try { await navigator.clipboard.writeText(text); return true; } catch { return false; } }
  async function completeBootstrap(input) {
    try {
      const response = await fetch('/api/v1/auth/bootstrap', { method: 'POST', headers, credentials: 'same-origin', body: JSON.stringify(input) });
      if (response.ok) { window.location.replace('/'); return null; }
      if (response.status === 429) return 'Trop de tentatives. Réessaie dans une minute.';
      const problem = await response.json();
      return problem.detail || 'Initialisation indisponible. Vérifie le code ou utilise ton lien personnel.';
    } catch { return 'Le réseau est indisponible. Vérifie ta connexion puis réessaie.'; }
  }
  return { login: { start, stop, copy, takeLinkSecret }, logout, revealSelectedDay, copyText, completeBootstrap, revealElement: element => element?.scrollIntoView({ block: 'center' }) };
})();
