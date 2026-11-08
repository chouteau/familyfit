using System.Text.Encodings.Web;
using FamilyFit.Core.Services;
using Microsoft.AspNetCore.Authentication;
using Microsoft.Extensions.Options;

namespace FamilyFit.Web.Services;

public sealed class AgentAuthenticationHandler(IOptionsMonitor<AuthenticationSchemeOptions> options, ILoggerFactory logger, UrlEncoder encoder, AgentAuthorization authorization)
    : AuthenticationHandler<AuthenticationSchemeOptions>(options, logger, encoder)
{
    protected override async Task<AuthenticateResult> HandleAuthenticateAsync()
    {
        var header = Request.Headers.Authorization.ToString();
        if (!header.StartsWith("Bearer ", StringComparison.OrdinalIgnoreCase)) return AuthenticateResult.NoResult();
        var principal = await authorization.AuthenticateAsync(header[7..], Context.RequestAborted);
        return principal == null ? AuthenticateResult.Fail("Clé MCP invalide ou accès désactivé.") : AuthenticateResult.Success(new AuthenticationTicket(principal, Scheme.Name));
    }
    protected override Task HandleChallengeAsync(AuthenticationProperties properties)
    {
        Response.StatusCode = 401;
        Response.Headers.WWWAuthenticate = "Bearer realm=\"FamilyFit MCP\"";
        return Task.CompletedTask;
    }
}
