EXTERNAL_NET = 'any'
HOME_NET = '__HOME_NET__'

include '/etc/snort/snort_defaults.lua'

HTTP_PORTS = '__HTTP_PORTS__'

default_variables.ports.HTTP_PORTS = HTTP_PORTS
default_variables.ports.FILE_DATA_PORTS = HTTP_PORTS .. MAIL_PORTS

ips =
{
    enable_builtin_rules = true,
    mode = inline,

    variables = default_variables,
    rules = [[
        include /etc/snort/snort3-community-rules/snort3-community.rules
    ]]
}

daq =
{
    modules =
    {
        {
            name = 'nfq',
            mode = 'inline'
        }
    },
    inputs = { '1' }
}
