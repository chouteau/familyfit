using ChannelMediator;
using System.Security.Claims;
using FamilyFit.Contracts.Models;

namespace FamilyFit.Contracts.Models.Authentication;

public sealed record ApproveBrowserLoginRequest(ClaimsPrincipal User, string Code) : IRequest<Outcome<bool>>, IAuthorizedRequest, IHumanOnlyRequest { public bool AdminOnly => false; }
