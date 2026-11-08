using ChannelMediator;
using System.Security.Claims;
using FamilyFit.Contracts.Models;

namespace FamilyFit.Contracts.Models.Members;

public sealed record SaveMemberRequest(ClaimsPrincipal User, Guid? Id, MemberInput Input) : IRequest<Outcome<Member>>, IAuthorizedRequest, IChangesFamily { public bool AdminOnly => Id == null; }
