using ChannelMediator;
using System.Security.Claims;
using FamilyFit.Contracts;
using FamilyFit.Contracts.Models;

namespace FamilyFit.Contracts.Models.Members;

public static class MembersExtensions
{
    public static Task<Outcome<Member>> SaveMember(this IMediator mediator, ClaimsPrincipal User, Guid? Id, MemberInput Input, CancellationToken cancellationToken = default) => mediator.Send(new SaveMemberRequest(User, Id, Input), cancellationToken);
}
