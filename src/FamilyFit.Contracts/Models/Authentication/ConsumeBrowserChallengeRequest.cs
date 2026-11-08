using ChannelMediator;
using System.Security.Claims;
using FamilyFit.Contracts.Models;

namespace FamilyFit.Contracts.Models.Authentication;

public sealed record ConsumeBrowserChallengeRequest(Guid Id, string BrowserSecret) : IRequest<Outcome<BrowserLoginResult>>;
