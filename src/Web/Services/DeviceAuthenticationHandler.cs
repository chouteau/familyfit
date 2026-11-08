using System.Text.Encodings.Web;
using FamilyFit.Core.Services;
using Microsoft.AspNetCore.Authentication;
using Microsoft.Extensions.Options;

namespace FamilyFit.Web.Services;

public sealed class DeviceAuthenticationHandler(IOptionsMonitor<AuthenticationSchemeOptions> options, ILoggerFactory logger, UrlEncoder encoder, AuthService auth)
    : AuthenticationHandler<AuthenticationSchemeOptions>(options, logger, encoder)
{
    protected override async Task<AuthenticateResult> HandleAuthenticateAsync()
    {
        var header = Request.Headers.Authorization.ToString();
        if (!header.StartsWith("Bearer ", StringComparison.OrdinalIgnoreCase)) return AuthenticateResult.NoResult();
        var user = await auth.AuthenticateDeviceAsync(header[7..], Context.RequestAborted);
        return user == null ? AuthenticateResult.Fail("Téléphone non autorisé.") : AuthenticateResult.Success(new AuthenticationTicket(user, Scheme.Name));
    }
}
