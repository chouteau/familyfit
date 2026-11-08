using ChannelMediator;
using FamilyFit.Contracts;
using FamilyFit.Contracts.Models.Authentication;
using FamilyFit.Core.Services;

namespace FamilyFit.Core.Handlers.Authentication;

public sealed class CreateBrowserChallengeRequestHandler(AuthService auth) : IRequestHandler<CreateBrowserChallengeRequest, Outcome<BrowserChallengeWithSecret>>
{
    public async Task<Outcome<BrowserChallengeWithSecret>> Handle(CreateBrowserChallengeRequest request, CancellationToken ct)
    {
        try
        {
            var (grant, secret) = await auth.CreateChallengeAsync(request.LinkSecret, request.BrowserSecret, ct);
            return Outcome<BrowserChallengeWithSecret>.Ok(new(grant, secret));
        }
        catch (DomainException error) { return Outcome<BrowserChallengeWithSecret>.Fail(error.Message, error.Status); }
    }
}
