using ChannelMediator;
using System.Security.Claims;
using FamilyFit.Contracts.Models;

namespace FamilyFit.Contracts.Models.Authentication;

public sealed record CreateInvitationRequest(ClaimsPrincipal User, Guid MemberId) : IRequest<Outcome<InvitationGrant>>, IAuthorizedRequest, IHumanOnlyRequest { public bool AdminOnly => true; }
