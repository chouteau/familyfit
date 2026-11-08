using ChannelMediator;
using System.Security.Claims;
using FamilyFit.Contracts.Models;

namespace FamilyFit.Contracts.Models.Devices;

public sealed record GetDevicesRequest(ClaimsPrincipal User) : IRequest<Outcome<List<DeviceView>>>, IAuthorizedRequest, IHumanOnlyRequest { public bool AdminOnly => false; }
