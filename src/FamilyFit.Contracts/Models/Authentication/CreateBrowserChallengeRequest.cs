using ChannelMediator;
using System.Security.Claims;
using FamilyFit.Contracts.Models;

namespace FamilyFit.Contracts.Models.Authentication;

public sealed record CreateBrowserChallengeRequest(string LinkSecret, string? BrowserSecret = null) : IRequest<Outcome<BrowserChallengeWithSecret>>;
