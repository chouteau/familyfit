using ChannelMediator;
using System.Security.Claims;
using FamilyFit.Contracts.Models;

namespace FamilyFit.Contracts.Models.Authentication;

public sealed record EnrollDeviceRequest(EnrollRequest Input) : IRequest<Outcome<DeviceGrant>>;
