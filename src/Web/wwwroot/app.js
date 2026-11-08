window.familyFit = (() => {
  const headers = { 'X-FamilyFit-Request': '1' };
  let timer;
  let currentCode = '';
  const status = message => { const element = document.getElementById('login-status'); if (element) element.textContent = message; };
  const stop = () => { clearTimeout(timer); timer = undefined; };
  async function start() {
    stop();
    const qr = document.getElementById('login-qr');
    if (!qr) return;
    status('Préparation d’un nouveau code…');
    try {
      const response = await fetch('/api/v1/auth/browser/challenges', { method: 'POST', headers, credentials: 'same-origin' });
      if (!response.ok) throw new Error(response.status === 429 ? 'Trop de demandes. Réessaie dans une minute.' : 'Connexion au site impossible. Réessaie.');
      const data = await response.json();
      currentCode = data.code;
      qr.innerHTML = data.qrSvg;
      document.getElementById('login-code').textContent = `${currentCode.slice(0, 5)} ${currentCode.slice(5)}`;
      status('Code valable 5 minutes. Confirme la connexion dans ton application.');
      const deadline = new Date(data.expiresAtUtc).getTime();
      let failures = 0;
      async function poll() {
        if (!document.querySelector('[data-login]')) return;
        if (Date.now() >= deadline) { status('Ce code a expiré. Demande un nouveau code.'); return; }
        try {
          const answer = await fetch('/api/v1/auth/browser/consume', { method: 'POST', headers, credentials: 'same-origin' });
          if (!answer.ok) throw new Error('Connexion interrompue.');
          const result = await answer.json();
          failures = 0;
          if (result.state === 'approved') { status('Connexion confirmée. Bienvenue !'); window.location.replace('/'); return; }
          if (result.state === 'expired') { status('Ce code n’est plus valide. Demande un nouveau code.'); return; }
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
  return { login: { start, stop, copy }, logout, revealSelectedDay };
})();
