using System.Security.Claims;
using ChannelMediator;

namespace FamilyFit.Contracts.Models.Authentication;

public sealed record CreateMemberLoginLinkRequest(ClaimsPrincipal User, Guid MemberId) : IRequest<Outcome<MemberLoginLinkCreated>>, IAuthorizedRequest, IHumanOnlyRequest
{
    public bool AdminOnly => true;
}
