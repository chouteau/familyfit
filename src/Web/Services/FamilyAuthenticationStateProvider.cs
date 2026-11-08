using System.Security.Claims;
using FamilyFit.Contracts;
using FamilyFit.Core.Services;
using Microsoft.AspNetCore.Components.Authorization;
using Microsoft.AspNetCore.Components.Server;

namespace FamilyFit.Web.Services;

public sealed class FamilyAuthenticationStateProvider(ILoggerFactory loggerFactory, IServiceScopeFactory scopes) : RevalidatingServerAuthenticationStateProvider(loggerFactory)
{
    protected override TimeSpan RevalidationInterval => TimeSpan.FromSeconds(30);
    protected override async Task<bool> ValidateAuthenticationStateAsync(AuthenticationState authenticationState, CancellationToken cancellationToken)
    {
        await using var scope = scopes.CreateAsyncScope();
        try { await scope.ServiceProvider.GetRequiredService<AuthService>().RequireActorAsync(authenticationState.User, ct: cancellationToken); return true; }
        catch (DomainException) { return false; }
    }
}
