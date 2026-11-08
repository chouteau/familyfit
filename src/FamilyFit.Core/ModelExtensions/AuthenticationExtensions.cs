using ChannelMediator;
using System.Security.Claims;
using FamilyFit.Contracts;
using FamilyFit.Contracts.Models;

namespace FamilyFit.Contracts.Models.Authentication;

public static class AuthenticationExtensions
{
    public static Task<Outcome<BootstrapStatus>> GetBootstrapStatus(this IMediator mediator, CancellationToken cancellationToken = default) => mediator.Send(new GetBootstrapStatusRequest(), cancellationToken);
    public static Task<Outcome<BootstrapGrant>> CompleteBootstrap(this IMediator mediator, BootstrapInput input, CancellationToken cancellationToken = default) => mediator.Send(new CompleteBootstrapRequest(input), cancellationToken);
    public static Task<Outcome<InvitationPreview>> PreviewInvitation(this IMediator mediator, string code, CancellationToken cancellationToken = default) => mediator.Send(new PreviewInvitationRequest(code), cancellationToken);
    public static Task<Outcome<bool>> CloseBrowserSession(this IMediator mediator, ClaimsPrincipal user, CancellationToken cancellationToken = default) => mediator.Send(new CloseBrowserSessionRequest(user), cancellationToken);
    public static Task<Outcome<InvitationGrant>> CreateInvitation(this IMediator mediator, ClaimsPrincipal User, Guid MemberId, CancellationToken cancellationToken = default) => mediator.Send(new CreateInvitationRequest(User, MemberId), cancellationToken);
    public static Task<Outcome<bool>> ApproveBrowserLogin(this IMediator mediator, ClaimsPrincipal User, string Code, CancellationToken cancellationToken = default) => mediator.Send(new ApproveBrowserLoginRequest(User, Code), cancellationToken);
    public static Task<Outcome<Actor>> GetActor(this IMediator mediator, ClaimsPrincipal User, bool AdminOnly = false, CancellationToken cancellationToken = default) => mediator.Send(new GetActorRequest(User, AdminOnly), cancellationToken);
    public static Task<Outcome<DeviceGrant>> EnrollDevice(this IMediator mediator, EnrollRequest Input, CancellationToken cancellationToken = default) => mediator.Send(new EnrollDeviceRequest(Input), cancellationToken);
    public static Task<Outcome<BrowserChallengeWithSecret>> CreateBrowserChallenge(this IMediator mediator, string linkSecret, string? browserSecret = null, CancellationToken cancellationToken = default) => mediator.Send(new CreateBrowserChallengeRequest(linkSecret, browserSecret), cancellationToken);
    public static Task<Outcome<MemberLoginLinkCreated>> CreateMemberLoginLink(this IMediator mediator, ClaimsPrincipal user, Guid memberId, CancellationToken cancellationToken = default) => mediator.Send(new CreateMemberLoginLinkRequest(user, memberId), cancellationToken);
    public static Task<Outcome<MemberLoginPreview?>> PreviewMemberLoginLink(this IMediator mediator, string? linkSecret, string? browserSecret, CancellationToken cancellationToken = default) => mediator.Send(new PreviewMemberLoginLinkRequest(linkSecret, browserSecret), cancellationToken);
    public static Task<Outcome<BrowserLoginResult>> ConsumeBrowserChallenge(this IMediator mediator, Guid Id, string BrowserSecret, CancellationToken cancellationToken = default) => mediator.Send(new ConsumeBrowserChallengeRequest(Id, BrowserSecret), cancellationToken);
}
